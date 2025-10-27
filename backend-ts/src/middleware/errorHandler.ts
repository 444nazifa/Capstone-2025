import { Request, Response, NextFunction } from 'express';
import { ErrorResponse } from '../types/auth';

export const errorHandler = (
  err: Error,
  req: Request,
  res: Response<ErrorResponse>,
  next: NextFunction
): void => {
  console.error('Error:', err);

  if (res.headersSent) {
    return next(err);
  }

  const isDevelopment = process.env.NODE_ENV === 'development';

  res.status(500).json({
    success: false,
    message: isDevelopment ? err.message : 'Internal server error',
    ...(isDevelopment && { stack: err.stack })
  });
};

export const notFoundHandler = (
  req: Request,
  res: Response<ErrorResponse>
): void => {
  res.status(404).json({
    success: false,
    message: `Route ${req.method} ${req.path} not found`
  });
};