import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { supabaseAdmin } from '../config/supabase';
import { ErrorResponse } from '../types/auth';

export interface AuthenticatedRequest extends Request {
  user?: {
    id: string;
    email: string;
    name: string;
    date_of_birth: string;
  };
}

export const authenticateToken = async (
  req: AuthenticatedRequest,
  res: Response<ErrorResponse>,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
      res.status(401).json({
        success: false,
        message: 'Access token required'
      });
      return;
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET!) as {
      userId: string;
      email: string;
    };

    const { data: userData, error } = await supabaseAdmin
      .from('profiles')
      .select('*')
      .eq('id', decoded.userId)
      .single();

    if (error || !userData) {
      res.status(404).json({
        success: false,
        message: 'User not found'
      });
      return;
    }

    req.user = {
      id: userData.id,
      email: userData.email,
      name: userData.name,
      date_of_birth: userData.date_of_birth
    };

    next();
  } catch (error) {
    if (error instanceof jwt.JsonWebTokenError) {
      res.status(403).json({
        success: false,
        message: 'Invalid or expired token'
      });
    } else {
      res.status(500).json({
        success: false,
        message: 'Authentication error'
      });
    }
  }
};