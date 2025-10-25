import { Router, Request, Response } from 'express';
import { supabase, supabaseAdmin } from '../config/supabase';
import { registerSchema, loginSchema } from '../utils/validation';
import { RegisterRequest, LoginRequest, AuthResponse, ErrorResponse } from '../types/auth';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const router = Router();

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

    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      user: {
        id: profileData.id,
        email: profileData.email,
        name: profileData.name,
        date_of_birth: profileData.date_of_birth
      },
      token
    });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({
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

    res.json({
      success: true,
      message: 'Login successful',
      user: {
        id: profileData.id,
        email: profileData.email,
        name: profileData.name,
        date_of_birth: profileData.date_of_birth
      },
      token
    });

  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({
      success: false,
      message: 'Internal server error'
    });
  }
});

export default router;