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

    // Verify the user owns this booking
    if (booking.userId !== context.auth.uid) {
      throw new functions.https.HttpsError('permission-denied', 'Not authorized');
    }

    // Calculate duration
    const startTime = booking.startTime ? booking.startTime.toDate() : new Date();
    const endTime = new Date();
    const durationMs = endTime - startTime;
    const durationMinutes = Math.max(1, Math.floor(durationMs / 60000));

    // Fare: ₹2 per minute
    const fare = durationMinutes * 2.0;

    return { fare, durationMinutes };
  } catch (error) {
    console.error('Error calculating fare:', error);
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
