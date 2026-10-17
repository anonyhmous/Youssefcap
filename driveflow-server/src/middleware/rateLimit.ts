import rateLimit from 'express-rate-limit';
import { Request, Response } from 'express';

export const activateIpRateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 30, // 30 requests per IP per minute
  skip: () => process.env.NODE_ENV === 'test',
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req: Request, res: Response) => {
    res.status(429).json({
      error: 'RATE_LIMITED',
      message: 'Too many activation requests from this IP. Please try again later.',
    });
  },
});

export const activateDeviceRateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 10, // 10 requests per device per minute
  skip: () => process.env.NODE_ENV === 'test',
  keyGenerator: (req: Request) => {
    const headerDevice = req.headers['x-df-device'];
    if (typeof headerDevice === 'string' && headerDevice.length > 0) {
      return headerDevice;
    }
    if (req.body && typeof req.body.deviceId === 'string') {
      return req.body.deviceId;
    }
    return req.ip || 'unknown-device';
  },
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req: Request, res: Response) => {
    res.status(429).json({
      error: 'RATE_LIMITED',
      message: 'Too many activation requests for this device. Please try again later.',
    });
  },
});

export const adminLoginRateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  skip: () => process.env.NODE_ENV === 'test',
  standardHeaders: true,
  legacyHeaders: false,
  handler: (req: Request, res: Response) => {
    res.status(429).json({
      error: 'RATE_LIMITED',
      message: 'Too many login attempts. Please try again later.',
    });
  },
});
