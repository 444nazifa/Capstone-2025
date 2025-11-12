import firebaseService from './firebase.service';
import { getUserDeviceTokens, removeInvalidTokens } from '../routes/device-token.routes';

// NOTE: Not automatically called. Integrate with reminder scheduler to send notifications.

interface MedicationInfo {
  id: string;
  name: string;
  dosage?: string;
}

class NotificationService {
  private static instance: NotificationService;

  private constructor() {}

  static getInstance(): NotificationService {
    if (!NotificationService.instance) {
      NotificationService.instance = new NotificationService();
    }
    return NotificationService.instance;
  }

  async sendMedicationReminder(
    userId: string,
    medication: MedicationInfo,
    reminderId?: string
  ): Promise<boolean> {
    if (!firebaseService.isInitialized()) {
      console.log('⚠️  Push notifications disabled - Firebase not initialized');
      return false;
    }

    try {
      // Get user's device tokens
      const tokens = await getUserDeviceTokens(userId);

      if (tokens.length === 0) {
        console.log(`No device tokens found for user ${userId}`);
        return false;
      }

      // Prepare notification
      const title = 'Time for your medication';
      const body = medication.dosage
        ? `${medication.name} - ${medication.dosage}`
        : medication.name;

      const data: Record<string, string> = {
        type: 'medication_reminder',
        medication_id: medication.id
      };

      if (reminderId) {
        data.reminder_id = reminderId;
      }

      // Send to all user devices
      const result = await firebaseService.sendMulticastNotification(
        tokens,
        { title, body },
        data
      );

      // Remove invalid tokens
      if (result.invalidTokens.length > 0) {
        await removeInvalidTokens(result.invalidTokens);
      }

      console.log(`✅ Medication reminder sent to user ${userId}: ${result.successCount} delivered`);
      return result.successCount > 0;
    } catch (error) {
      console.error('Error sending medication reminder:', error);
      return false;
    }
  }

  async sendMissedMedicationAlert(
    userId: string,
    medication: MedicationInfo
  ): Promise<boolean> {
    if (!firebaseService.isInitialized()) {
      return false;
    }

    try {
      const tokens = await getUserDeviceTokens(userId);

      if (tokens.length === 0) {
        return false;
      }

      const title = 'Missed Medication';
      const body = `You missed taking ${medication.name}`;

      const data: Record<string, string> = {
        type: 'missed_medication',
        medication_id: medication.id
      };

      const result = await firebaseService.sendMulticastNotification(
        tokens,
        { title, body },
        data
      );

      if (result.invalidTokens.length > 0) {
        await removeInvalidTokens(result.invalidTokens);
      }

      return result.successCount > 0;
    } catch (error) {
      console.error('Error sending missed medication alert:', error);
      return false;
    }
  }

  async sendRefillReminder(
    userId: string,
    medication: MedicationInfo,
    remainingQuantity: number
  ): Promise<boolean> {
    if (!firebaseService.isInitialized()) {
      return false;
    }

    try {
      const tokens = await getUserDeviceTokens(userId);

      if (tokens.length === 0) {
        return false;
      }

      const title = 'Time to refill medication';
      const body = `${medication.name} is running low (${remainingQuantity} remaining)`;

      const data: Record<string, string> = {
        type: 'refill_reminder',
        medication_id: medication.id,
        remaining_quantity: remainingQuantity.toString()
      };

      const result = await firebaseService.sendMulticastNotification(
        tokens,
        { title, body },
        data
      );

      if (result.invalidTokens.length > 0) {
        await removeInvalidTokens(result.invalidTokens);
      }

      return result.successCount > 0;
    } catch (error) {
      console.error('Error sending refill reminder:', error);
      return false;
    }
  }

  async sendTestNotification(userId: string): Promise<boolean> {
    if (!firebaseService.isInitialized()) {
      console.log('Firebase not initialized');
      return false;
    }

    try {
      const tokens = await getUserDeviceTokens(userId);

      if (tokens.length === 0) {
        console.log('No device tokens found');
        return false;
      }

      const result = await firebaseService.sendMulticastNotification(
        tokens,
        {
          title: 'Test Notification',
          body: 'Push notifications are working correctly!'
        },
        {
          type: 'test'
        }
      );

      if (result.invalidTokens.length > 0) {
        await removeInvalidTokens(result.invalidTokens);
      }

      return result.successCount > 0;
    } catch (error) {
      console.error('Error sending test notification:', error);
      return false;
    }
  }
}

export default NotificationService.getInstance();
