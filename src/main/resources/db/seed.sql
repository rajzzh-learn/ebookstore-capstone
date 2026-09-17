-- =====================================================================
-- E-Bookstore – Seed Data
-- Run after the application starts and creates tables via hibernate ddl-auto=update
-- =====================================================================

-- Categories
INSERT INTO categories (name, description)
VALUES
  ('Fiction',        'Novels and literary fiction'),
  ('Non-Fiction',    'Biographies, memoirs, essays'),
  ('Science',        'Physics, biology, mathematics and more'),
  ('Technology',     'Programming, AI, data science'),
  ('Self-Help',      'Personal growth and productivity'),
  ('History',        'World history, civilisations, wars'),
  ('Children',       'Books for young readers'),
  ('Comics',         'Graphic novels and manga')
ON CONFLICT (name) DO NOTHING;

-- Brands (Publishers)
INSERT INTO brands (name, logo_url)
VALUES
  ('Penguin Random House', NULL),
  ('HarperCollins',        NULL),
  ('Macmillan Publishers', NULL),
  ('O''Reilly Media',      NULL),
  ('Wiley',                NULL),
  ('Scholastic',           NULL)
ON CONFLICT (name) DO NOTHING;

-- Products (Sample Books)
INSERT INTO products (title, author, description, price, stock_quantity, image_url,
                      estimated_delivery_days, category_id, brand_id)
SELECT
  p.title, p.author, p.description, p.price, p.stock,
  p.image_url, p.delivery_days,
  (SELECT id FROM categories WHERE name = p.cat),
  (SELECT id FROM brands   WHERE name = p.brand)
FROM (VALUES
  ('Clean Code',
   'Robert C. Martin',
   'A handbook of agile software craftsmanship.',
   35.99, 50, NULL, 5, 'Technology', 'O''Reilly Media'),

  ('The Pragmatic Programmer',
   'David Thomas & Andrew Hunt',
   'Your journey to mastery.',
   39.99, 40, NULL, 5, 'Technology', 'O''Reilly Media'),

  ('Designing Data-Intensive Applications',
   'Martin Kleppmann',
   'The big ideas behind reliable, scalable and maintainable systems.',
   49.99, 30, NULL, 5, 'Technology', 'O''Reilly Media'),

  ('A Brief History of Time',
   'Stephen Hawking',
   'A landmark volume in science writing.',
   14.99, 100, NULL, 4, 'Science', 'Penguin Random House'),

  ('Sapiens',
   'Yuval Noah Harari',
   'A brief history of humankind.',
   17.99, 80, NULL, 4, 'History', 'HarperCollins'),

  ('Atomic Habits',
   'James Clear',
   'An easy and proven way to build good habits and break bad ones.',
   16.99, 120, NULL, 4, 'Self-Help', 'Penguin Random House'),

  ('Harry Potter and the Philosopher''s Stone',
   'J.K. Rowling',
   'The first book in the Harry Potter series.',
   12.99, 200, NULL, 3, 'Fiction', 'Scholastic'),

  ('1984',
   'George Orwell',
   'A dystopian social science fiction novel.',
   11.99, 150, NULL, 4, 'Fiction', 'Penguin Random House'),

  ('The Great Gatsby',
   'F. Scott Fitzgerald',
   'A classic American novel set in the Jazz Age.',
   10.99, 90, NULL, 4, 'Fiction', 'Macmillan Publishers'),

  ('Dune',
   'Frank Herbert',
   'An epic science fiction saga.',
   18.99, 60, NULL, 5, 'Fiction', 'Macmillan Publishers')
) AS p(title, author, description, price, stock, image_url, delivery_days, cat, brand)
ON CONFLICT DO NOTHING;
