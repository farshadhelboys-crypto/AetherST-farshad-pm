-- لایسنس‌های زمانی + حجمی
-- license_type: 'time' | 'volume' | 'both'
CREATE TABLE IF NOT EXISTS licenses (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT NOT NULL UNIQUE,
  device_id TEXT,
  license_type TEXT NOT NULL DEFAULT 'time',
  duration_days INTEGER NOT NULL DEFAULT 30,
  volume_gb REAL NOT NULL DEFAULT 0,
  used_bytes INTEGER NOT NULL DEFAULT 0,
  expires_at INTEGER NOT NULL DEFAULT 0,
  active INTEGER NOT NULL DEFAULT 1,
  activated_at INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_licenses_device ON licenses(device_id);
CREATE INDEX IF NOT EXISTS idx_licenses_active ON licenses(active);
CREATE INDEX IF NOT EXISTS idx_licenses_code ON licenses(code);

-- مهاجرت از اسکیمای قدیمی (اگر جدول از قبل وجود دارد):
-- ALTER TABLE licenses ADD COLUMN license_type TEXT NOT NULL DEFAULT 'time';
-- ALTER TABLE licenses ADD COLUMN volume_gb REAL NOT NULL DEFAULT 0;
-- ALTER TABLE licenses ADD COLUMN used_bytes INTEGER NOT NULL DEFAULT 0;
