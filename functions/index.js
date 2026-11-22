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

    // Fare calculation aligned with base: ₹5 base + ₹8/km + ₹1/min, min ₹10
    const distanceKm = Math.min(booking.distanceKm || 0, 200.0);
    const baseFare = 5.0;
    const ratePerKm = 8.0;
    const ratePerMin = 1.0;
    const minFare = 10.0;

    let fare = baseFare + (distanceKm * ratePerKm) + (durationMinutes * ratePerMin);
    fare = Math.max(fare, minFare);
    fare = Math.round(fare * 100.0) / 100.0;

    return { fare, durationMinutes };
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
