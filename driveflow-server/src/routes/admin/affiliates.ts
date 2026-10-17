import { Router, Request, Response, NextFunction } from 'express';
import { z } from 'zod';
import { requireAdminAuth } from '../../middleware/auth.js';
import { getDb } from '../../db/database.js';
import { recordAudit } from '../../middleware/audit.js';

export const adminAffiliatesRouter = Router();

function formatAffiliate(a: any) {
  return {
    id: a.id,
    code: a.code,
    ownerName: a.ownerName,
    discountText: a.discountText || null,
    giftDays: a.giftDays,
    maxUses: a.maxUses !== null ? Number(a.maxUses) : null,
    uses: a.uses,
    expiresAt: a.expiresAt || null,
  };
}

const affiliateCreateSchema = z.object({
  code: z.string().min(1).max(32),
  ownerName: z.string().min(1),
  discountText: z.string().nullable().optional(),
  giftDays: z.number().int().min(0).default(0),
  maxUses: z.number().int().min(1).nullable().optional(),
  expiresAt: z.string().datetime().nullable().optional(),
});

const affiliatePatchSchema = affiliateCreateSchema.partial();

// List affiliates
adminAffiliatesRouter.get(
  '/admin/api/affiliates',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const db = getDb();
      const rows = db.prepare('SELECT * FROM affiliates ORDER BY createdAt DESC').all() as any[];
      res.status(200).json(rows.map(formatAffiliate));
    } catch (err) {
      next(err);
    }
  }
);

// Create affiliate
adminAffiliatesRouter.post(
  '/admin/api/affiliates',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const parsed = affiliateCreateSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const { code, ownerName, discountText, giftDays, maxUses, expiresAt } = parsed.data;
      const db = getDb();

      // Check duplicate code
      const existing = db.prepare('SELECT id FROM affiliates WHERE code = ?').get(code.trim());
      if (existing) {
        res.status(409).json({ error: 'CONFLICT', message: 'Affiliate code already exists' });
        return;
      }

      const nowIso = new Date().toISOString();
      const stmt = db.prepare(`
        INSERT INTO affiliates (code, ownerName, discountText, giftDays, maxUses, uses, expiresAt, createdAt)
        VALUES (?, ?, ?, ?, ?, 0, ?, ?)
      `);

      const info = stmt.run(
        code.trim().toUpperCase(),
        ownerName.trim(),
        discountText || null,
        giftDays,
        maxUses ?? null,
        expiresAt || null,
        nowIso
      );

      const created = db.prepare('SELECT * FROM affiliates WHERE id = ?').get(info.lastInsertRowid);
      recordAudit(req.admin!.username, `create affiliate code=${code} for ${ownerName}`);

      res.status(201).json(formatAffiliate(created));
    } catch (err) {
      next(err);
    }
  }
);

// Update affiliate
adminAffiliatesRouter.patch(
  '/admin/api/affiliates/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const parsed = affiliatePatchSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          error: 'BAD_REQUEST',
          message: `Invalid input: ${parsed.error.issues.map(i => i.message).join(', ')}`,
        });
        return;
      }

      const db = getDb();
      const affiliate = db.prepare('SELECT * FROM affiliates WHERE id = ?').get(id) as any;
      if (!affiliate) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Affiliate not found' });
        return;
      }

      const updates = parsed.data;
      db.prepare(`
        UPDATE affiliates SET
          code = COALESCE(?, code),
          ownerName = COALESCE(?, ownerName),
          discountText = COALESCE(?, discountText),
          giftDays = COALESCE(?, giftDays),
          maxUses = COALESCE(?, maxUses),
          expiresAt = COALESCE(?, expiresAt)
        WHERE id = ?
      `).run(
        updates.code?.trim().toUpperCase() ?? null,
        updates.ownerName?.trim() ?? null,
        updates.discountText ?? null,
        updates.giftDays ?? null,
        updates.maxUses ?? null,
        updates.expiresAt ?? null,
        id
      );

      const updated = db.prepare('SELECT * FROM affiliates WHERE id = ?').get(id);
      recordAudit(req.admin!.username, `update affiliate id=${id}`);
      res.status(200).json(formatAffiliate(updated));
    } catch (err) {
      next(err);
    }
  }
);

// Delete affiliate
adminAffiliatesRouter.delete(
  '/admin/api/affiliates/:id',
  requireAdminAuth,
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      const db = getDb();
      const affiliate = db.prepare('SELECT * FROM affiliates WHERE id = ?').get(id) as any;
      if (!affiliate) {
        res.status(404).json({ error: 'NOT_FOUND', message: 'Affiliate not found' });
        return;
      }

      db.prepare('DELETE FROM affiliates WHERE id = ?').run(id);
      recordAudit(req.admin!.username, `delete affiliate id=${id} (${affiliate.code})`);
      res.status(200).json({ success: true });
    } catch (err) {
      next(err);
    }
  }
);
