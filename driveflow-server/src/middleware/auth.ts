import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { loadEnv } from '../config/env.js';

export interface AdminPayload {
  username: string;
  iat?: number;
  exp?: number;
}

declare global {
  namespace Express {
    interface Request {
      admin?: AdminPayload;
    }
  }
}

export function requireAdminAuth(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Admin authorization header required (Bearer <token>)',
    });
    return;
  }

  const token = authHeader.substring(7).trim();
  const config = loadEnv();

  try {
    const decoded = jwt.verify(token, config.JWT_SECRET) as AdminPayload;
    req.admin = decoded;
    next();
  } catch {
    res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Invalid or expired admin token',
    });
  }
}
