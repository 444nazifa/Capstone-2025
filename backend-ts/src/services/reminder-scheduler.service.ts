import { supabaseAdmin } from '../config/supabase';
import notificationService from './notification.service';

// NOTE: Scheduler is NOT automatically started. Call startScheduler() to enable.

class ReminderSchedulerService {
  private static instance: ReminderSchedulerService;
  private schedulerInterval: NodeJS.Timeout | null = null;
  private isRunning = false;
  private readonly CHECK_INTERVAL = 60 * 1000;

  private constructor() {}

  static getInstance(): ReminderSchedulerService {
    if (!ReminderSchedulerService.instance) {
      ReminderSchedulerService.instance = new ReminderSchedulerService();
    }
    return ReminderSchedulerService.instance;
  }

  startScheduler(): void {
    if (this.isRunning) {
      console.log('⚠️  Reminder scheduler is already running');
      return;
    }

    console.log('📅 Starting reminder scheduler...');
    this.isRunning = true;

    // Run immediately on start
    this.checkDueReminders();

    // Then run at regular intervals
    this.schedulerInterval = setInterval(() => {
      this.checkDueReminders();
    }, this.CHECK_INTERVAL);

    console.log(`✅ Reminder scheduler started (checking every ${this.CHECK_INTERVAL / 1000}s)`);
  }

  stopScheduler(): void {
    if (!this.isRunning) {
      console.log('⚠️  Reminder scheduler is not running');
      return;
    }

    if (this.schedulerInterval) {
      clearInterval(this.schedulerInterval);
      this.schedulerInterval = null;
    }

    this.isRunning = false;
    console.log('✅ Reminder scheduler stopped');
  }

  private async checkDueReminders(): Promise<void> {
    try {
      const now = new Date();
      const oneMinuteAgo = new Date(now.getTime() - 60 * 1000);

      // Get due schedules from medication_schedules
      const { data: schedules, error: schedulesError } = await supabaseAdmin
        .from('medication_schedules')
        .select(`
          id,
          user_id,
          medication_id,
          time,
          enabled,
          user_medications!inner (
            id,
            name,
            dosage,
            is_active
          )
        `)
        .eq('enabled', true)
        .eq('user_medications.is_active', true)
        .lte('time', now.toISOString())
        .gte('time', oneMinuteAgo.toISOString());

      if (schedulesError) {
        console.error('Error fetching schedules:', schedulesError);
        return;
      }

      if (!schedules || schedules.length === 0) {
        console.log('✅ No due reminders at', now.toISOString());
        return;
      }

      // Filter out reminders that have already been taken/skipped
      const remindersToSend = [];
      for (const schedule of schedules) {
        const { data: history, error: historyError } = await supabaseAdmin
          .from('medication_history')
          .select('id')
          .eq('medication_id', schedule.medication_id)
          .eq('scheduled_time', schedule.time)
          .in('status', ['taken', 'skipped'])
          .limit(1);

        if (historyError) {
          console.error('Error checking history:', historyError);
          continue;
        }

        // If no history found, reminder needs to be sent
        if (!history || history.length === 0) {
          remindersToSend.push(schedule);
        }
      }

      if (remindersToSend.length === 0) {
        console.log('✅ No new reminders to send at', now.toISOString());
        return;
      }

      console.log(`📬 Found ${remindersToSend.length} due reminder(s)`);

      // Send notifications for each due reminder
      for (const reminder of remindersToSend) {
        try {
          const medication = Array.isArray(reminder.user_medications)
            ? reminder.user_medications[0]
            : reminder.user_medications;

          if (!medication) {
            console.warn(`⚠️  No medication found for schedule ${reminder.id}`);
            continue;
          }

          const success = await notificationService.sendMedicationReminder(
            reminder.user_id,
            {
              id: reminder.medication_id,
              name: medication.name,
              dosage: medication.dosage
            },
            reminder.id
          );

          if (success) {
            console.log(`✅ Reminder sent for medication: ${medication.name}`);
          } else {
            console.log(`⚠️  No devices to notify for user: ${reminder.user_id}`);
          }
        } catch (error) {
          console.error(`❌ Error sending reminder:`, error);
        }
      }
    } catch (error) {
      console.error('❌ Error checking due reminders:', error);
    }
  }

  async triggerCheck(): Promise<void> {
    console.log('🔄 Manually triggering reminder check...');
    await this.checkDueReminders();
  }

  isSchedulerRunning(): boolean {
    return this.isRunning;
  }
}

export default ReminderSchedulerService.getInstance();
