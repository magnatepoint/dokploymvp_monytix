# Monytix MVP

A Next.js application for Monytix - Your Personal Finance Companion.

## Features

- 🎨 **Splash Screen** - 3-second branded splash screen with Monytix logo
- 🔐 **Authentication** - Firebase Auth (email/password and Google); backend uses Firebase ID token and UID for spendsense, goaltracker, budgetpilot, moneymoments
- 🔗 **Backend Integration** - Connected to Monytix backend API
- 📱 **Responsive Design** - Modern UI with Tailwind CSS

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm, yarn, pnpm, or bun
- Firebase project (for web app auth)
- Backend API (expects Firebase ID token in Authorization header)

### Installation

1. Install dependencies:

```bash
npm install
```

2. Set up environment variables:

Create a `.env.local` file in the root directory. See [ENV_SETUP.md](./ENV_SETUP.md) for detailed instructions.

```env
# Firebase (required for login; same project as Android/iOS)
NEXT_PUBLIC_FIREBASE_API_KEY=your-api-key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your-project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=123456789
NEXT_PUBLIC_FIREBASE_APP_ID=1:123456789:web:abc123

NEXT_PUBLIC_API_URL=https://backend.monytix.ai
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
