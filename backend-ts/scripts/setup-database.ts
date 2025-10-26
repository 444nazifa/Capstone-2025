import { Client } from 'pg';
import dotenv from 'dotenv';

dotenv.config();

async function setupDatabase() {
  console.log('Setting up database schema...');

  // Extract connection details from the pooler URL you provided
  const client = new Client({
    connectionString: 'process.env.DATABASE_URL',
    ssl: { rejectUnauthorized: false }
  });

  try {
    await client.connect();
    console.log('Connected to database');

    // Create profiles table
    await client.query(`
      CREATE TABLE IF NOT EXISTS public.profiles (
        id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL,
        date_of_birth DATE NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
      );
    `);
    console.log('✅ profiles table created');

    // Enable Row Level Security
    await client.query(`ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;`);
    console.log('✅ Row Level Security enabled');

    // Create policies
    await client.query(`
      DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
      CREATE POLICY "Users can view own profile" ON public.profiles
        FOR SELECT USING (auth.uid() = id);
    `);

    await client.query(`
      DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
      CREATE POLICY "Users can update own profile" ON public.profiles
        FOR UPDATE USING (auth.uid() = id);
    `);
    console.log('✅ Policies created');

    // Create updated_at function and trigger
    await client.query(`
      CREATE OR REPLACE FUNCTION public.handle_updated_at()
      RETURNS TRIGGER AS $$
      BEGIN
        NEW.updated_at = NOW();
        RETURN NEW;
      END;
      $$ LANGUAGE plpgsql;
    `);

    await client.query(`
      DROP TRIGGER IF EXISTS profiles_updated_at ON public.profiles;
      CREATE TRIGGER profiles_updated_at
        BEFORE UPDATE ON public.profiles
        FOR EACH ROW
        EXECUTE FUNCTION public.handle_updated_at();
    `);
    console.log('✅ Updated timestamp trigger created');

    console.log('\n🎉 Database schema setup completed successfully!');

  } catch (error) {
    console.error('Setup failed:', error);
    process.exit(1);
  } finally {
    await client.end();
  }
}

setupDatabase();