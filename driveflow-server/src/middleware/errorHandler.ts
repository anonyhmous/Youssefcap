import { Request, Response, NextFunction } from 'express';
import { ZodError } from 'zod';

export function errorHandler(
  err: unknown,
  req: Request,
  res: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  next: NextFunction
): void {
  if (err instanceof ZodError) {
    const message = err.issues.map(e => `${e.path.join('.')}: ${e.message}`).join(', ');
    res.status(400).json({
      error: 'BAD_REQUEST',
      message: `Validation failed: ${message}`,
    });
    return;
  }

  if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 400) {
    res.status(400).json({
      error: 'BAD_REQUEST',
      message: (err as any).message || 'Bad Request',
    });
    return;
  }

  console.error('Unhandled server error:', err);
  res.status(500).json({
    error: 'INTERNAL_ERROR',
    message: 'An unexpected internal server error occurred',
  });
}
