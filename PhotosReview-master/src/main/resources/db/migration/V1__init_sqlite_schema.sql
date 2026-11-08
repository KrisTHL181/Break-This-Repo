CREATE TABLE IF NOT EXISTS review_users (
    uid INTEGER PRIMARY KEY,
    password TEXT NOT NULL,
    login_time INTEGER NOT NULL,
    allname TEXT NOT NULL,
    status INTEGER NOT NULL
);

INSERT INTO review_users(uid, password, login_time, allname, status)
SELECT 10000, '$2a$10$3WrOZ89bEfspgEEm3.u5Ku5DDJHfHQ1dFb4c9F83NbhRv6Wp6W2He', 0, 'Administrator', 0
WHERE NOT EXISTS (
    SELECT 1 FROM review_users WHERE uid = 10000
);

CREATE TABLE IF NOT EXISTS review_proj (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    projid TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL UNIQUE,
    type INTEGER NOT NULL,
    max INTEGER NOT NULL DEFAULT 4,
    task TEXT NOT NULL,
    recheck TEXT NOT NULL,
    thumbnail TEXT NOT NULL,
    status INTEGER NOT NULL,
    time TEXT NOT NULL,
    display INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS review_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    proj TEXT NOT NULL,
    author TEXT NOT NULL,
    value TEXT NOT NULL
);

CREATE INDEX idx_review_data_proj ON review_data(proj);

INSERT OR IGNORE INTO sqlite_sequence(name, seq) VALUES ('review_data', 999);

CREATE TABLE IF NOT EXISTS review_config (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    value TEXT NOT NULL
);

INSERT INTO review_config(id, name, value)
SELECT 1, 'website_name', 'DemoB'
WHERE NOT EXISTS (
    SELECT 1 FROM review_config WHERE id = 1
);

INSERT INTO review_config(id, name, value)
SELECT 2, 'website_icon', 'data/icon/default_icon.png'
WHERE NOT EXISTS (
    SELECT 1 FROM review_config WHERE id = 2
);

CREATE TABLE IF NOT EXISTS review_recheck (
    photoid INTEGER NOT NULL PRIMARY KEY,
    proj TEXT NOT NULL,
    value TEXT NOT NULL,
    final_score REAL NOT NULL
);

CREATE INDEX idx_review_recheck_proj ON review_recheck(proj);

CREATE TABLE IF NOT EXISTS review_result (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    proj TEXT NOT NULL UNIQUE,
    value TEXT NOT NULL,
    time TEXT NOT NULL
);
