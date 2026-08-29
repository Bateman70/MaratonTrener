-- ====================================================
-- MARATHONTRAINER SUPABASE POSTGRESQL SCHEMA
-- ====================================================

-- 1. PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    nickname TEXT,
    email TEXT,
    avatar_url TEXT,
    age INT,
    weight NUMERIC,
    max_hr INT,
    pb_10k TEXT,
    pb_half TEXT,
    pb_full TEXT,
    current_race_name TEXT,
    current_race_date DATE,
    plan_start_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. WORKOUTS TABLE
CREATE TABLE IF NOT EXISTS public.workouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    scheduled_date DATE NOT NULL,
    week_number INT DEFAULT 1,
    workout_type TEXT NOT NULL,
    distance NUMERIC DEFAULT 0,
    total_duration INT DEFAULT 0,
    avg_heart_rate INT DEFAULT 0,
    description TEXT,
    notes TEXT,
    is_completed BOOLEAN DEFAULT false,
    shoe_id TEXT,
    strava_activity_id TEXT,
    strava_polyline TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. BUDDIES TABLE
CREATE TABLE IF NOT EXISTS public.buddies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    buddy_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'accepted' CHECK (status IN ('pending', 'accepted')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    UNIQUE(user_id, buddy_id)
);

-- ====================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.buddies ENABLE ROW LEVEL SECURITY;

-- Profiles: Anyone authenticated can view profiles (for Buddy search), only owner can edit
CREATE POLICY "Public profiles are viewable by authenticated users" 
ON public.profiles FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can insert/update their own profile" 
ON public.profiles FOR ALL TO authenticated USING (auth.uid() = id);

-- Workouts: Owner has full access; Buddies can read completed workouts
CREATE POLICY "Users can manage their own workouts" 
ON public.workouts FOR ALL TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "Buddies can view completed workouts" 
ON public.workouts FOR SELECT TO authenticated 
USING (
    is_completed = true AND 
    user_id IN (
        SELECT buddy_id FROM public.buddies WHERE user_id = auth.uid() AND status = 'accepted'
    )
);

-- Buddies: Users can manage their own buddy connections
CREATE POLICY "Users can manage their buddy list" 
ON public.buddies FOR ALL TO authenticated USING (auth.uid() = user_id);

-- Trigger: Automatically create profile entry on new Auth user sign-up
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name, avatar_url)
  VALUES (
    new.id,
    new.email,
    COALESCE(new.raw_user_meta_data->>'full_name', new.raw_user_meta_data->>'name', 'Runner'),
    new.raw_user_meta_data->>'avatar_url'
  );
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();
