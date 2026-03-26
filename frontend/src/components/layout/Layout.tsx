import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';

export function Layout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-border bg-muted/40 py-8 mt-auto">
        <div className="w-full max-w-[1600px] mx-auto px-4 md:px-8 xl:px-12 text-center text-sm text-muted-foreground">
          © {new Date().getFullYear()} E-Commerce Store. All rights reserved.
        </div>
      </footer>
    </div>
  );
}
