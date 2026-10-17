import Database from 'better-sqlite3';

export interface Migration {
  version: number;
  name: string;
  up: (db: Database.Database) => void;
}

export const migrations: Migration[] = [
  {
    version: 1,
    name: 'initial_schema',
    up: (db: Database.Database) => {
      // 1. Devices
      db.exec(`
        CREATE TABLE IF NOT EXISTS devices (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          deviceId TEXT NOT NULL UNIQUE,
          name TEXT,
          phone TEXT,
          governorate TEXT,
          model TEXT,
          androidSdk INTEGER,
          appVersionName TEXT,
          lang TEXT DEFAULT 'ar',
          status TEXT NOT NULL DEFAULT 'TRIAL',
          trialEndsAt TEXT,
          planEndsAt TEXT,
          giftEndsAt TEXT,
          killed INTEGER NOT NULL DEFAULT 0,
          revoked INTEGER NOT NULL DEFAULT 0,
          keyId INTEGER,
          firstSeenAt TEXT NOT NULL,
          lastSeenAt TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_devices_deviceId ON devices(deviceId);
        CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
      `);

      // 2. License Keys
      db.exec(`
        CREATE TABLE IF NOT EXISTS license_keys (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          key TEXT NOT NULL UNIQUE,
          planDays INTEGER NOT NULL DEFAULT 30,
          status TEXT NOT NULL DEFAULT 'UNUSED',
          note TEXT,
          boundDeviceId TEXT,
          activatedAt TEXT,
          expiresAt TEXT,
          lastBoundAt TEXT,
          createdAt TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_license_keys_key ON license_keys(key);
        CREATE INDEX IF NOT EXISTS idx_license_keys_boundDeviceId ON license_keys(boundDeviceId);
      `);

      // 3. Affiliates & Affiliate Uses
      db.exec(`
        CREATE TABLE IF NOT EXISTS affiliates (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          code TEXT NOT NULL UNIQUE,
          ownerName TEXT NOT NULL,
          discountText TEXT,
          giftDays INTEGER NOT NULL DEFAULT 0,
          maxUses INTEGER,
          uses INTEGER NOT NULL DEFAULT 0,
          expiresAt TEXT,
          createdAt TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_affiliates_code ON affiliates(code);

        CREATE TABLE IF NOT EXISTS affiliate_uses (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          affiliateId INTEGER NOT NULL,
          deviceId TEXT NOT NULL,
          usedAt TEXT NOT NULL,
          FOREIGN KEY(affiliateId) REFERENCES affiliates(id) ON DELETE CASCADE
        );
        CREATE INDEX IF NOT EXISTS idx_affiliate_uses_device ON affiliate_uses(deviceId);
        CREATE UNIQUE INDEX IF NOT EXISTS idx_affiliate_uses_code_device ON affiliate_uses(affiliateId, deviceId);
      `);

      // 4. Events
      db.exec(`
        CREATE TABLE IF NOT EXISTS events (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          deviceId TEXT NOT NULL,
          type TEXT NOT NULL,
          value TEXT NOT NULL,
          app TEXT NOT NULL,
          ref TEXT,
          ts TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_events_deviceId ON events(deviceId);
        CREATE INDEX IF NOT EXISTS idx_events_ts ON events(ts);
      `);

      // 5. Reports
      db.exec(`
        CREATE TABLE IF NOT EXISTS reports (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          deviceId TEXT NOT NULL,
          category TEXT NOT NULL,
          message TEXT NOT NULL,
          facts TEXT,
          hasImage INTEGER NOT NULL DEFAULT 0,
          status TEXT NOT NULL DEFAULT 'OPEN',
          adminNote TEXT,
          createdAt TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_reports_deviceId ON reports(deviceId);
        CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
      `);

      // 6. News
      db.exec(`
        CREATE TABLE IF NOT EXISTS news (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          title TEXT NOT NULL,
          body TEXT NOT NULL,
          url TEXT,
          published INTEGER NOT NULL DEFAULT 1,
          createdAt TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_news_createdAt ON news(createdAt);
      `);

      // 7. Reader Configs
      db.exec(`
        CREATE TABLE IF NOT EXISTS reader_configs (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          app TEXT NOT NULL UNIQUE,
          revision INTEGER NOT NULL DEFAULT 1,
          config_json TEXT NOT NULL,
          updatedAt TEXT NOT NULL
        );
      `);

      // 8. Releases
      db.exec(`
        CREATE TABLE IF NOT EXISTS releases (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          versionCode INTEGER NOT NULL UNIQUE,
          versionName TEXT NOT NULL,
          notes TEXT NOT NULL,
          mandatory INTEGER NOT NULL DEFAULT 0,
          sha256 TEXT NOT NULL,
          sizeBytes INTEGER NOT NULL,
          createdAt TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_releases_versionCode ON releases(versionCode);
      `);

      // 9. Settings
      db.exec(`
        CREATE TABLE IF NOT EXISTS settings (
          key TEXT PRIMARY KEY,
          value TEXT NOT NULL
        );
      `);

      // 10. Admin Audit
      db.exec(`
        CREATE TABLE IF NOT EXISTS admin_audit (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          who TEXT NOT NULL,
          what TEXT NOT NULL,
          when_timestamp TEXT NOT NULL
        );
      `);

      // Insert default settings
      const insertSetting = db.prepare('INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)');
      insertSetting.run('trialDays', '7');
      insertSetting.run('freeDailyList', '20');
      insertSetting.run('freeDailyDetails', '5');
      insertSetting.run('minVersionCode', '1');
      insertSetting.run('priceText', '');
      insertSetting.run('supportWhatsapp', '');
      insertSetting.run('giftDaysDefault', '0');
      insertSetting.run('expectedSignerSha256', '');

      // Insert default reader configurations for the 3 supported apps
      const now = new Date().toISOString();
      const insertConfig = db.prepare('INSERT OR IGNORE INTO reader_configs (app, revision, config_json, updatedAt) VALUES (?, ?, ?, ?)');
      
      const inDriveDefault = {
        app: 'indrive',
        listContainerIds: ['sinet.startup.inDriver:id/rv_orders', 'sinet.startup.inDriver:id/recycler_view'],
        itemCardIds: ['sinet.startup.inDriver:id/order_card_container'],
        fareViewIds: ['sinet.startup.inDriver:id/tv_price'],
        pickupDistanceIds: ['sinet.startup.inDriver:id/tv_pickup_distance'],
        tripDistanceIds: ['sinet.startup.inDriver:id/tv_distance', 'sinet.startup.inDriver:id/tv_trip_distance'],
        pickupAddressIds: ['sinet.startup.inDriver:id/tv_from_address'],
        destinationAddressIds: ['sinet.startup.inDriver:id/tv_to_address'],
        ratingIds: ['sinet.startup.inDriver:id/tv_rating'],
        tripsCountIds: ['sinet.startup.inDriver:id/tv_trips_count'],
        paymentMethodIds: ['sinet.startup.inDriver:id/tv_payment_type'],
        riderNoteIds: ['sinet.startup.inDriver:id/tv_comment'],
        requestAgeIds: ['sinet.startup.inDriver:id/tv_time_ago'],
        offersCountIds: ['sinet.startup.inDriver:id/tv_offers_count'],
        extraStopsIds: ['sinet.startup.inDriver:id/tv_extra_stops'],
        mapContainerIds: ['sinet.startup.inDriver:id/map_container'],
      };

      const uberDefault = {
        app: 'uber',
        listContainerIds: ['com.ubercab.driver:id/offers_list'],
        itemCardIds: ['com.ubercab.driver:id/offer_card'],
        fareViewIds: ['com.ubercab.driver:id/fare_amount'],
        pickupDistanceIds: ['com.ubercab.driver:id/pickup_eta_distance'],
        tripDistanceIds: ['com.ubercab.driver:id/trip_distance'],
        pickupAddressIds: ['com.ubercab.driver:id/pickup_address'],
        destinationAddressIds: ['com.ubercab.driver:id/destination_address'],
        ratingIds: ['com.ubercab.driver:id/rider_rating'],
      };

      const didiDefault = {
        app: 'didi',
        listContainerIds: ['com.didiglobal.driver:id/order_list'],
        itemCardIds: ['com.didiglobal.driver:id/order_card'],
        fareViewIds: ['com.didiglobal.driver:id/tv_fare'],
        pickupDistanceIds: ['com.didiglobal.driver:id/tv_pickup_km'],
        tripDistanceIds: ['com.didiglobal.driver:id/tv_trip_km'],
        pickupAddressIds: ['com.didiglobal.driver:id/tv_start_address'],
        destinationAddressIds: ['com.didiglobal.driver:id/tv_end_address'],
      };

      insertConfig.run('indrive', 1, JSON.stringify(inDriveDefault), now);
      insertConfig.run('uber', 1, JSON.stringify(uberDefault), now);
      insertConfig.run('didi', 1, JSON.stringify(didiDefault), now);
    },
  },
];

export function runMigrations(db: Database.Database): void {
  db.exec(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      version INTEGER PRIMARY KEY,
      applied_at TEXT NOT NULL
    );
  `);

  const appliedRows = db.prepare('SELECT version FROM schema_migrations').all() as { version: number }[];
  const appliedVersions = new Set(appliedRows.map(r => r.version));

  for (const migration of migrations) {
    if (!appliedVersions.has(migration.version)) {
      console.log(`Applying migration ${migration.version}: ${migration.name}...`);
      db.transaction(() => {
        migration.up(db);
        db.prepare('INSERT INTO schema_migrations (version, applied_at) VALUES (?, ?)').run(
          migration.version,
          new Date().toISOString()
        );
      })();
    }
  }
}
