import { Router, Request, Response } from 'express';
import { supabaseAdmin } from '../config/supabase';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';

const router = Router();

// NOTE: These routes are NOT automatically called. Frontend must explicitly enable notifications.
router.post('/', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        error: 'User not authenticated'
      });
    }

    const userId = req.user.id;
    const { token, platform } = req.body;

    if (!token || !platform) {
      return res.status(400).json({
        success: false,
        error: 'Token and platform are required'
      });
    }

    if (!['android', 'ios'].includes(platform)) {
      return res.status(400).json({
        success: false,
        error: 'Platform must be either "android" or "ios"'
      });
    }

    const { data: deviceToken, error } = await supabaseAdmin
      .from('device_tokens')
      .upsert({
        user_id: userId,
        token: token,
        platform: platform,
        last_used_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      }, {
        onConflict: 'token'
      })
      .select()
      .single();

    if (error) {
      console.error('Error registering device token:', error);
      return res.status(500).json({
        success: false,
        error: 'Failed to register device token'
      });
    }

    console.log(`✅ Device token registered for user ${userId} (${platform})`);

    return res.status(200).json({
      success: true,
      message: 'Device token registered successfully',
      deviceToken
    });
  } catch (error: any) {
    console.error('Error registering device token:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to register device token'
    });
  }
});

router.delete('/:token', async (req: Request, res: Response) => {
  try {
    const { token } = req.params;

    if (!token) {
      return res.status(400).json({
        success: false,
        error: 'Token is required'
      });
    }

    const { data, error } = await supabaseAdmin
      .from('device_tokens')
      .delete()
      .eq('token', token)
      .select()
      .single();

    if (error || !data) {
      return res.status(404).json({
        success: false,
        error: 'Device token not found'
      });
    }

    console.log(`✅ Device token unregistered: ${token.substring(0, 20)}...`);

    return res.status(200).json({
      success: true,
      message: 'Device token unregistered successfully'
    });
  } catch (error: any) {
    console.error('Error unregistering device token:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to unregister device token'
    });
  }
});

router.get('/', authenticateToken, async (req: AuthenticatedRequest, res: Response) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        error: 'User not authenticated'
      });
    }

    const userId = req.user.id;

    const { data: deviceTokens, error } = await supabaseAdmin
      .from('device_tokens')
      .select('id, token, platform, created_at, last_used_at')
      .eq('user_id', userId)
      .order('created_at', { ascending: false });

    if (error) {
      console.error('Error fetching device tokens:', error);
      return res.status(500).json({
        success: false,
        error: 'Failed to fetch device tokens'
      });
    }

    return res.status(200).json({
      success: true,
      deviceTokens: deviceTokens || []
    });
  } catch (error: any) {
    console.error('Error fetching device tokens:', error);
    return res.status(500).json({
      success: false,
      error: 'Failed to fetch device tokens'
    });
  }
});

// Helper: Get all device tokens for a user
export async function getUserDeviceTokens(userId: string): Promise<string[]> {
  try {
    const { data, error } = await supabaseAdmin
      .from('device_tokens')
      .select('token')
      .eq('user_id', userId);

    if (error) {
      console.error('Error fetching user device tokens:', error);
      return [];
    }

    return data?.map((row: { token: string }) => row.token) || [];
  } catch (error) {
    console.error('Error fetching user device tokens:', error);
    return [];
  }
}

// Helper: Remove invalid tokens (called when FCM returns error)
export async function removeInvalidTokens(tokens: string[]): Promise<void> {
  if (tokens.length === 0) return;

  try {
    const { error } = await supabaseAdmin
      .from('device_tokens')
      .delete()
      .in('token', tokens);

    if (error) {
      console.error('Error removing invalid tokens:', error);
      return;
    }

    console.log(`✅ Removed ${tokens.length} invalid device token(s)`);
  } catch (error) {
    console.error('Error removing invalid tokens:', error);
  }
}

export default router;
