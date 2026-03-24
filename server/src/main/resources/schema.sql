-- Jörmungandr Server Database Schema
-- SQLite with WAL mode for concurrent reads

-- Player save data (raw JSON blobs from client)
CREATE TABLE IF NOT EXISTS players (
    access_code TEXT PRIMARY KEY,
    data        TEXT NOT NULL,
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Room data (raw JSON blobs, keyed by room ID like "r3_04250")
CREATE TABLE IF NOT EXISTS rooms (
    room_id    TEXT PRIMARY KEY,
    region     INTEGER NOT NULL DEFAULT -1,
    data       TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_rooms_region ON rooms(region);

-- Player notes per room (JSON array of note objects)
CREATE TABLE IF NOT EXISTS notes (
    room_id    TEXT PRIMARY KEY,
    data       TEXT NOT NULL DEFAULT '[]',
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Trade listings per room (JSON array, hub room only in practice)
CREATE TABLE IF NOT EXISTS trades (
    room_id    TEXT PRIMARY KEY,
    data       TEXT NOT NULL DEFAULT '[]',
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Co-location action logs per room (JSON array, TTL-pruned)
CREATE TABLE IF NOT EXISTS actions (
    room_id    TEXT PRIMARY KEY,
    data       TEXT NOT NULL DEFAULT '[]',
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
