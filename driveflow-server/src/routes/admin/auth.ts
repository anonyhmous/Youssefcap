import { Router, Request, Response, NextFunction } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { loadEnv } from '../../config/env.js';
import { adminLoginRateLimiter } from '../../middleware/rateLimit.js';

export const adminAuthRouter = Router();

const loginSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
});

adminAuthRouter.post(
  '/admin/api/login',
  adminLoginRateLimiter,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const parsed = loginSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: 'Username and password are required',
        });
        return;
      }

      const { username, password } = parsed.data;
      const config = loadEnv();

      if (username !== config.ADMIN_USERNAME) {
        res.status(401).json({
          error: 'INVALID_CREDENTIALS',
          message: 'Invalid username or password',
        });
        return;
      }

      const isMatch = bcrypt.compareSync(password, config.ADMIN_PASSWORD_HASH);
      if (!isMatch) {
        res.status(401).json({
          error: 'INVALID_CREDENTIALS',
          message: 'Invalid username or password',
        });
        return;
      }

      const expiresInSeconds = 12 * 3600; // 12 hours
      const expiresAt = new Date(Date.now() + expiresInSeconds * 1000).toISOString();

      const token = jwt.sign(
        { username: config.ADMIN_USERNAME },
        config.JWT_SECRET,
        { expiresIn: expiresInSeconds }
      );

      res.status(200).json({
        token,
        expiresAt,
      });
    } catch (err) {
      next(err);
    }
  }
);
