import { Router, Request, Response } from 'express';
import { supabase, supabaseAdmin } from '../config/supabase';
import { registerSchema, loginSchema, updateProfileSchema } from '../utils/validation';
import { RegisterRequest, LoginRequest, UpdateProfileRequest, AuthResponse, ErrorResponse } from '../types/auth';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const router = Router();

console.log('[BOOT] Loaded auth routes');  

router.get('/ping', (_req, res) => res.json({ pong: true }));

router.post('/register', async (req: Request<{}, AuthResponse | ErrorResponse, RegisterRequest>, res: Response<AuthResponse | ErrorResponse>) => {
  try {
    const { error, value } = registerSchema.validate(req.body);
    
    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: error.details.map(detail => detail.message)
      });
    }

    const { name, email, password, date_of_birth } = value;

    const { data: existingUser } = await supabaseAdmin
      .from('profiles')
      .select('email')
      .eq('email', email)
      .single();

    if (existingUser) {
      return res.status(409).json({
        success: false,
        message: 'User with this email already exists'
      });
    }

    const { data: authData, error: authError } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          name,
          date_of_birth
        },
        emailRedirectTo: undefined
      }
    });

    if (authError) {
      return res.status(400).json({
        success: false,
        message: authError.message
      });
    }

    if (!authData.user) {
      return res.status(400).json({
        success: false,
        message: 'Failed to create user'
      });
    }

    const { data: profileData, error: profileError } = await supabaseAdmin
      .from('profiles')
      .insert({
        id: authData.user.id,
        email,
        name,
        date_of_birth
      })
      .select()
      .single();

    if (profileError) {
      await supabaseAdmin.auth.admin.deleteUser(authData.user.id);
      return res.status(500).json({
        success: false,
        message: 'Failed to create user profile'
      });
    }

    const token = jwt.sign(
      { userId: authData.user.id, email },
      process.env.JWT_SECRET!,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' } as jwt.SignOptions
    );

    return res.status(201).json({
      success: true,
      message: 'User registered successfully',
      user: {
        id: profileData.id,
        email: profileData.email,
        name: profileData.name,
        date_of_birth: profileData.date_of_birth,
        phone: profileData.phone
      },
      token
    });

  } catch (error) {
    console.error('Registration error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.post('/login', async (req: Request<{}, AuthResponse | ErrorResponse, LoginRequest>, res: Response<AuthResponse | ErrorResponse>) => {
  try {
    const { error, value } = loginSchema.validate(req.body);
    
    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: error.details.map(detail => detail.message)
      });
    }

    const { email, password } = value;

    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email,
      password
    });

    if (authError || !authData.user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password'
      });
    }

    const { data: profileData, error: profileError } = await supabaseAdmin
      .from('profiles')
      .select('*')
      .eq('id', authData.user.id)
      .single();

    if (profileError || !profileData) {
      return res.status(404).json({
        success: false,
        message: 'User profile not found'
      });
    }

    const token = jwt.sign(
      { userId: authData.user.id, email },
      process.env.JWT_SECRET!,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' } as jwt.SignOptions
    );

    return res.json({
      success: true,
      message: 'Login successful',
      user: {
        id: profileData.id,
        email: profileData.email,
        name: profileData.name,
        date_of_birth: profileData.date_of_birth,
        phone: profileData.phone
      },
      token
    });

  } catch (error) {
    console.error('Login error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.get('/profile', authenticateToken, async (req: AuthenticatedRequest, res: Response<AuthResponse | ErrorResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    return res.json({
      success: true,
      message: 'Profile retrieved successfully',
      user: {
        id: req.user.id,
        email: req.user.email,
        name: req.user.name,
        date_of_birth: req.user.date_of_birth,
        phone: req.user.phone
      }
    });
  } catch (error) {
    console.error('Get profile error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

router.put('/profile', authenticateToken, async (req: AuthenticatedRequest, res: Response<AuthResponse | ErrorResponse>) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated'
      });
    }

    const { error, value } = updateProfileSchema.validate(req.body);

    if (error) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: error.details.map(detail => detail.message)
      });
    }

    const updateData: Partial<UpdateProfileRequest> = {};

    if (value.name !== undefined) updateData.name = value.name;
    if (value.email !== undefined) updateData.email = value.email;
    if (value.date_of_birth !== undefined) updateData.date_of_birth = value.date_of_birth;
    if (value.phone !== undefined) updateData.phone = value.phone;

    if (Object.keys(updateData).length === 0) {
      return res.status(400).json({
        success: false,
        message: 'No fields to update'
      });
    }

    // If email is being updated, check if it's already in use
    if (updateData.email && updateData.email !== req.user.email) {
      const { data: existingUser } = await supabaseAdmin
        .from('profiles')
        .select('email')
        .eq('email', updateData.email)
        .single();

      if (existingUser) {
        return res.status(409).json({
          success: false,
          message: 'Email already in use'
        });
      }

      // Update Supabase auth email
      const { error: authError } = await supabaseAdmin.auth.admin.updateUserById(
        req.user.id,
        { email: updateData.email }
      );

      if (authError) {
        return res.status(400).json({
          success: false,
          message: 'Failed to update email in authentication system'
        });
      }
    }

    // Update profile in database
    const { data: updatedProfile, error: updateError } = await supabaseAdmin
      .from('profiles')
      .update(updateData)
      .eq('id', req.user.id)
      .select()
      .single();

    if (updateError || !updatedProfile) {
      return res.status(500).json({
        success: false,
        message: 'Failed to update profile'
      });
    }

    return res.json({
      success: true,
      message: 'Profile updated successfully',
      user: {
        id: updatedProfile.id,
        email: updatedProfile.email,
        name: updatedProfile.name,
        date_of_birth: updatedProfile.date_of_birth,
        phone: updatedProfile.phone
      }
    });

  } catch (error) {
    console.error('Update profile error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

// Forgot Password route
router.post('/forgot-password', async (req: Request, res: Response) => {
  try {
    const { email } = req.body;

    if (!email || typeof email !== 'string') {
      return res.status(400).json({
        success: false,
        message: 'Email is required'
      });
    }

    // Check email format 
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid email format'
      });
    }

    // Checking if user exists 
    const { data: existingUser } = await supabaseAdmin
      .from('profiles')
      .select('email')
      .eq('email', email)
      .single();

    if (!existingUser) {
      return res.json({
        success: true,
        message: 'If an account exists with this email, a password reset link has been sent'
      });
    }
    // Send password reset email 
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${process.env.FRONTEND_URL || 'http://localhost:3000'}/reset-password`
    });

    if (error) {
      console.error('Password reset error:', error);
      return res.status(500).json({
        success: false,
        message: 'Failed to send password reset email'
      });
    }

    return res.json({
      success: true,
      message: 'If an account exists with this email, a password reset link has been sent'
    });

  } catch (error) {
    console.error('Forgot password error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

//Reset password
router.post('/reset-password', async (req: Request, res: Response) => {
  try {
    const { token, password } = req.body;

    if (!token || !password) {
      return res.status(400).json({
        success: false,
        message: 'Token and new password are required'
      });
    }

    // Allow for strong password, check length 
    if (password.length < 8) {
      return res.status(400).json({
        success: false,
        message: 'Password must be at least 8 characters long'
      });
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/;
    if (!passwordRegex.test(password)) {
      return res.status(400).json({
        success: false,
        message: 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character'
      });
    }

    // verify the token and get the user 
    const { data: { user }, error: getUserError } = await supabase.auth.getUser(token);
    
    if (getUserError || !user) {
      return res.status(400).json({
        success: false,
        message: 'Invalid or expired reset token'
      });
    }

    // Update users password 
    const { error: updateError } = await supabaseAdmin.auth.admin.updateUserById(
      user.id,
      { password: password }
    );

    if (updateError) {
      console.error('Reset password error:', updateError);
      return res.status(400).json({
        success: false,
        message: updateError.message || 'Failed to reset password'
      });
    }

    return res.json({
      success: true,
      message: 'Password has been reset successfully'
    });

  } catch (error) {
    console.error('Reset password error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

//Check if token is valid 
router.post('/verify-reset-token', async (req: Request, res: Response) => {
  try {
    const { token } = req.body;

    if (!token) {
      return res.status(400).json({
        success: false,
        message: 'Token is required'
      });
    }

    //verify token 
    const { data: { user }, error } = await supabase.auth.getUser(token);

    if (error || !user) {
      return res.status(400).json({
        success: false,
        message: 'Invalid or expired token'
      });
    }

    return res.json({
      success: true,
      message: 'Token is valid',
      user: {
        id: user.id,
        email: user.email
      }
    });

  } catch (error) {
    console.error('Verify token error:', error);
    return res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});
export default router;