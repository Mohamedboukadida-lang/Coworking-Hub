import { Compass } from 'lucide-react';
import { Link, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { Layout } from './components/Layout';
import { EmptyState } from './components/States';
import { LoginPage, RegisterPage } from './pages/AuthPages';
import { BookingsPage } from './pages/BookingsPage';
import { ExplorePage } from './pages/ExplorePage';
import { ManagePage } from './pages/ManagePage';
import { ManageSpacePage } from './pages/ManageSpacePage';
import { NewSpacePage } from './pages/NewSpacePage';
import { ProfilePage } from './pages/ProfilePage';
import { SpacePage } from './pages/SpacePage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<ExplorePage />} />
        <Route path="spaces/:id" element={<SpacePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="bookings" element={<RequireAuth><BookingsPage /></RequireAuth>} />
        <Route path="profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />
        <Route path="manage" element={<RequireAuth owner><ManagePage /></RequireAuth>} />
        <Route path="manage/new" element={<RequireAuth owner><NewSpacePage /></RequireAuth>} />
        <Route path="manage/spaces/:id" element={<RequireAuth owner><ManageSpacePage /></RequireAuth>} />
        <Route
          path="*"
          element={
            <EmptyState icon={<Compass size={24} />} title="Page not found" action={<Link className="btn" to="/">Explore spaces</Link>}>
              The page you're looking for doesn't exist.
            </EmptyState>
          }
        />
      </Route>
    </Routes>
  );
}
