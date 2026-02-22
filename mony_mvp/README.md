# Monytix MVP

A Next.js application for Monytix - Your Personal Finance Companion.

## Features

- 🎨 **Splash Screen** - 3-second branded splash screen with Monytix logo
- 🔐 **Authentication** - Supabase authentication with Google OAuth sign-in
- 🔗 **Backend Integration** - Connected to Monytix backend API
- 📱 **Responsive Design** - Modern UI with Tailwind CSS

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm, yarn, pnpm, or bun
- Supabase account and project
- Backend API access

### Installation

1. Install dependencies:

```bash
npm install
```

2. Set up environment variables:

Create a `.env.local` file in the root directory. See [ENV_SETUP.md](./ENV_SETUP.md) for detailed instructions.

```env
NEXT_PUBLIC_SUPABASE_URL=your-supabase-url
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-supabase-anon-key
NEXT_PUBLIC_API_URL=http://34.14.136.76:8001
NEXT_PUBLIC_SUPABASE_REDIRECT_URL=http://localhost:3000/auth/callback
```

3. Run the development server:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

## Project Structure

```
mony_mvp/
├── app/                    # Next.js App Router pages
│   ├── auth/              # Authentication routes
│   ├── layout.tsx         # Root layout with metadata
│   └── page.tsx           # Main page with auth flow
├── components/            # React components
│   ├── AuthScreen.tsx    # Login/Registration screen
│   └── SplashScreen.tsx  # Splash screen component
├── lib/                   # Utility libraries
│   ├── api/              # Backend API client
│   └── supabase/         # Supabase client setup
└── public/               # Static assets
    ├── monytix.png       # Brand logo
    └── favicon.ico       # Site favicon
```

## Authentication Flow

1. **Splash Screen** - Shows Monytix logo for 3 seconds
2. **Auth Screen** - Login/Registration with Google OAuth
3. **Session Validation** - Validates session with backend API
4. **Main App** - User dashboard (to be implemented)

## Tech Stack

- **Framework**: Next.js 16 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Authentication**: Supabase Auth
- **Backend**: Monytix API (FastAPI)

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
