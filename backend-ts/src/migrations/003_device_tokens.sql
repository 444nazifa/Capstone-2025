-- Device Tokens Table for Push Notifications
-- Run this migration to enable push notification support

CREATE TABLE IF NOT EXISTS device_tokens (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  token TEXT NOT NULL UNIQUE,
  platform VARCHAR(20) NOT NULL CHECK (platform IN ('android', 'ios')),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  last_used_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for fast lookups by user
CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id ON device_tokens(user_id);

-- Index for token lookups
CREATE INDEX IF NOT EXISTS idx_device_tokens_token ON device_tokens(token);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_device_tokens_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at
CREATE TRIGGER device_tokens_updated_at
  BEFORE UPDATE ON device_tokens
  FOR EACH ROW
  EXECUTE FUNCTION update_device_tokens_updated_at();

-- Comments for documentation
COMMENT ON TABLE device_tokens IS 'Stores FCM/APNs device tokens for push notifications';
COMMENT ON COLUMN device_tokens.user_id IS 'Reference to the user who owns this device';
COMMENT ON COLUMN device_tokens.token IS 'FCM or APNs device token';
COMMENT ON COLUMN device_tokens.platform IS 'Platform: android or ios';
COMMENT ON COLUMN device_tokens.last_used_at IS 'Last time this token was used to send a notification';
