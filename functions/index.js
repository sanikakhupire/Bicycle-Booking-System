/**
 * Firebase Cloud Functions for Bicycle Booking App
 * 
 * Setup:
 * 1. cd functions
 * 2. npm install
 * 3. firebase deploy --only functions
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

/**
 * calculateFare
 * 
 * Callable function that calculates the fare for a completed ride.
 * Server-authoritative fare calculation aligned with FareCalculator.
 * 
 * Input: { bookingId: string }
 * Output: { fare: number, durationMinutes: number }
 */
exports.calculateFare = functions.https.onCall(async (data, context) => {
  // Ensure user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { bookingId } = data;

  if (!bookingId) {
    throw new functions.https.HttpsError('invalid-argument', 'bookingId is required');
  }

  try {
    const bookingRef = db.collection('bookings').doc(bookingId);
    const bookingSnap = await bookingRef.get();

    if (!bookingSnap.exists) {
      throw new functions.https.HttpsError('not-found', 'Booking not found');
    }

    const booking = bookingSnap.data();

    // Verify the user owns this booking or is admin
    if (booking.userId !== context.auth.uid) {
      const callerDoc = await db.collection('users').doc(context.auth.uid).get();
      const isAdmin = callerDoc.exists && (callerDoc.data().isAdmin || callerDoc.data().role === 'ADMIN');
      if (!isAdmin) {
        throw new functions.https.HttpsError('permission-denied', 'Not authorized');
      }
    }

    // Calculate duration
    const startTime = booking.startTime ? booking.startTime.toDate() : new Date();
    const endTime = new Date();
    const durationMs = Math.max(0, endTime - startTime);
    const durationMinutes = Math.max(1, Math.floor(durationMs / 60000));

    // Fetch pricing configuration snapshot or default config
    let pricing = booking.pricingSnapshot;
    if (!pricing) {
      const configSnap = await db.collection('pricing_config').doc('default').get();
      if (configSnap.exists) {
        pricing = configSnap.data();
      } else {
        pricing = {
          baseFeePaise: 1000,
          ratePerMinutePaise: 200,
          ratePerHourPaise: 6000,
          minimumFarePaise: 2000,
          maximumDurationMinutes: 720,
          lateFeePerHourPaise: 5000,
          taxEnabled: false,
          taxPercentage: 5.0,
          standardCategoryMultiplier: 1.0,
          electricCategoryMultiplier: 1.5,
          mountainCategoryMultiplier: 1.25
        };
      }
    }

    // Category multiplier
    const category = (booking.bicycleType || 'Standard').toUpperCase();
    let multiplier = 1.0;
    if (category.includes('ELECTRIC') || category.includes('E-BIKE')) {
      multiplier = pricing.electricCategoryMultiplier || 1.5;
    } else if (category.includes('MOUNTAIN') || category.includes('GEARED')) {
      multiplier = pricing.mountainCategoryMultiplier || 1.25;
    } else {
      multiplier = pricing.standardCategoryMultiplier || 1.0;
    }

    // Base fee
    const baseFeePaise = pricing.baseFeePaise || 1000;

    // Usage charge
    let usageChargePaise = 0;
    const ratePerMin = pricing.ratePerMinutePaise || 200;
    const ratePerHour = pricing.ratePerHourPaise || 6000;
    if (durationMinutes < 60) {
      usageChargePaise = Math.round(durationMinutes * ratePerMin * multiplier);
    } else {
      const hours = Math.floor(durationMinutes / 60);
      const remMins = durationMinutes % 60;
      let minComp = remMins * ratePerMin;
      minComp = Math.min(minComp, ratePerHour);
      usageChargePaise = Math.round(((hours * ratePerHour) + minComp) * multiplier);
    }

    // Late return penalty
    let lateFeePaise = 0;
    const maxDuration = pricing.maximumDurationMinutes || 720;
    if (maxDuration > 0 && durationMinutes > maxDuration) {
      const lateMins = durationMinutes - maxDuration;
      const lateHours = Math.ceil(lateMins / 60);
      lateFeePaise = lateHours * (pricing.lateFeePerHourPaise || 5000);
    }

    // Subtotal
    const subtotalPaise = baseFeePaise + usageChargePaise + lateFeePaise;

    // Coupon discount if present
    let discountPaise = 0;
    if (booking.couponCode) {
      const couponSnap = await db.collection('coupons').where('code', '==', booking.couponCode.toUpperCase()).limit(1).get();
      if (!couponSnap.empty) {
        const cData = couponSnap.docs[0].data();
        if (cData.active) {
          if (cData.discountType === 'PERCENTAGE') {
            discountPaise = Math.round(subtotalPaise * (cData.discountPercentage / 100.0));
            if (cData.maxDiscountPaise > 0) discountPaise = Math.min(discountPaise, cData.maxDiscountPaise);
          } else {
            discountPaise = Math.min(cData.discountValuePaise || 1000, subtotalPaise);
          }
        }
      }
    }

    // Minimum ride fare guarantee
    const minimumFarePaise = pricing.minimumFarePaise || 2000;
    const afterDiscount = Math.max(0, subtotalPaise - discountPaise);
    const taxablePaise = Math.max(minimumFarePaise, afterDiscount);

    // Taxes (GST)
    let totalTaxPaise = 0;
    if (pricing.taxEnabled && pricing.taxPercentage > 0) {
      totalTaxPaise = Math.round(taxablePaise * (pricing.taxPercentage / 100.0));
    }

    const totalFarePaise = taxablePaise + totalTaxPaise;
    const fare = Math.round(totalFarePaise) / 100.0;
    const subtotal = Math.round(subtotalPaise) / 100.0;
    const discount = Math.round(discountPaise) / 100.0;
    const tax = Math.round(totalTaxPaise) / 100.0;

    return {
      fare,
      durationMinutes,
      subtotal,
      discount,
      tax,
      totalFarePaise,
      distanceKm: booking.distanceKm || 0.0
    };
  } catch (error) {
    console.error('Error calculating fare:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * expireStaleBookings
 * 
 * Callable function to batch-expire stale pending bookings (>10 mins hold)
 * and safely release bicycles back into station inventory.
 */
exports.expireStaleBookings = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
  }

  try {
    const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000);
    const staleSnap = await db.collection('bookings')
      .where('status', '==', 'PENDING')
      .where('createdAt', '<=', tenMinutesAgo)
      .limit(50)
      .get();

    let expiredCount = 0;

    for (const doc of staleSnap.docs) {
      const booking = doc.data();
      const bookingRef = doc.ref;

      await db.runTransaction(async (t) => {
        t.update(bookingRef, { status: 'EXPIRED' });

        if (booking.bicycleId) {
          const bikeRef = db.collection('bicycles').doc(booking.bicycleId);
          t.update(bikeRef, { available: true, status: 'AVAILABLE' });
        }

        if (booking.stationId) {
          const stationRef = db.collection('stations').doc(booking.stationId);
          t.update(stationRef, { availableBicycles: admin.firestore.FieldValue.increment(1) });
        }
      });

      expiredCount++;
    }

    return { success: true, expiredCount };
  } catch (error) {
    console.error('Error expiring stale bookings:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * sendBookingNotification
 * 
 * Triggered when a new booking is created.
 * Sends a push notification to the user.
 */
exports.sendBookingNotification = functions.firestore
  .document('bookings/{bookingId}')
  .onCreate(async (snap, context) => {
    const booking = snap.data();
    const userId = booking.userId;

    // Get user's FCM token from Firestore (you'd store this in the user document)
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) return;

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) return;

    const message = {
      notification: {
        title: 'Booking Confirmed!',
        body: `Your bicycle at ${booking.stationName} is ready. Scan the QR code to start.`,
      },
      token: fcmToken,
    };

    try {
      await admin.messaging().send(message);
      console.log('Notification sent to user:', userId);
    } catch (error) {
      console.error('Error sending notification:', error);
    }
  });
