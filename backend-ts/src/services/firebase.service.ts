import admin from 'firebase-admin';
import path from 'path';
import fs from 'fs';

// NOTE: Firebase is NOT automatically initialized. Call initializeFirebase() to enable push notifications.

class FirebaseService {
  private static instance: FirebaseService;
  private initialized = false;

  private constructor() {}

  static getInstance(): FirebaseService {
    if (!FirebaseService.instance) {
      FirebaseService.instance = new FirebaseService();
    }
    return FirebaseService.instance;
  }

  initializeFirebase(): boolean {
    if (this.initialized) {
      console.log('Firebase already initialized');
      return true;
    }

    try {
      const serviceAccountPath = path.join(__dirname, '../../firebase-admin-key.json');

      if (!fs.existsSync(serviceAccountPath)) {
        console.warn('⚠️  Firebase service account key not found at:', serviceAccountPath);
        console.warn('⚠️  Push notifications are DISABLED. To enable:');
        console.warn('    1. Download service account key from Firebase Console');
        console.warn('    2. Save as firebase-admin-key.json in backend-ts directory');
        console.warn('    3. Restart the server');
        return false;
      }

      const serviceAccount = require(serviceAccountPath);

      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });

      this.initialized = true;
      console.log('✅ Firebase Admin SDK initialized successfully');
      return true;
    } catch (error) {
      console.error('❌ Failed to initialize Firebase Admin SDK:', error);
      return false;
    }
  }

  isInitialized(): boolean {
    return this.initialized;
  }

  async sendNotification(
    token: string,
    notification: {
      title: string;
      body: string;
    },
    data?: Record<string, string>
  ): Promise<void> {
    if (!this.initialized) {
      console.warn('Cannot send notification: Firebase not initialized');
      return;
    }

    try {
      const message: admin.messaging.Message = {
        token,
        notification: {
          title: notification.title,
          body: notification.body
        },
        data,
        android: {
          priority: 'high',
          notification: {
            channelId: 'medication_reminders',
            priority: 'high' as any,
            sound: 'default'
          }
        }
      };

      const response = await admin.messaging().send(message);
      console.log('✅ Notification sent successfully:', response);
    } catch (error: any) {
      console.error('❌ Failed to send notification:', error);

      if (error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-registration-token') {
        throw new Error('INVALID_TOKEN');
      }

      throw error;
    }
  }

  async sendMulticastNotification(
    tokens: string[],
    notification: {
      title: string;
      body: string;
    },
    data?: Record<string, string>
  ): Promise<{
    successCount: number;
    failureCount: number;
    invalidTokens: string[];
  }> {
    if (!this.initialized) {
      console.warn('Cannot send notification: Firebase not initialized');
      return { successCount: 0, failureCount: tokens.length, invalidTokens: [] };
    }

    if (tokens.length === 0) {
      return { successCount: 0, failureCount: 0, invalidTokens: [] };
    }

    try {
      const message: admin.messaging.MulticastMessage = {
        tokens,
        notification: {
          title: notification.title,
          body: notification.body
        },
        data,
        android: {
          priority: 'high',
          notification: {
            channelId: 'medication_reminders',
            priority: 'high' as any,
            sound: 'default'
          }
        }
      };

      const response = await admin.messaging().sendEachForMulticast(message);

      const invalidTokens: string[] = [];
      response.responses.forEach((resp, idx) => {
        if (!resp.success && resp.error) {
          const errorCode = resp.error.code;
          if (errorCode === 'messaging/registration-token-not-registered' ||
              errorCode === 'messaging/invalid-registration-token') {
            const token = tokens[idx];
            if (token) {
              invalidTokens.push(token);
            }
          }
        }
      });

      console.log(`✅ Multicast notification sent: ${response.successCount} success, ${response.failureCount} failure`);
      if (invalidTokens.length > 0) {
        console.log(`⚠️  Found ${invalidTokens.length} invalid tokens`);
      }

      return {
        successCount: response.successCount,
        failureCount: response.failureCount,
        invalidTokens
      };
    } catch (error) {
      console.error('❌ Failed to send multicast notification:', error);
      throw error;
    }
  }
}

export default FirebaseService.getInstance();
