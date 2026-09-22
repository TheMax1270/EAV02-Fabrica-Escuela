import { Route, Routes } from 'react-router-dom'
import HomePage from '../pages/HomePage'
import RegisterPage from '../pages/RegisterPage'
import LoginPage from '../pages/LoginPage'
import AccountPage from '../pages/AccountPage'
import ProfilePage from '../pages/ProfilePage'
import AdminUsersPage from '../pages/AdminUsersPage'
import ProtectedRoute from '../components/ProtectedRoute'
import MyProjectsPage from '../pages/MyProjectsPage'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/account" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
      <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
      <Route path="/admin/users" element={<ProtectedRoute role="ADMIN"><AdminUsersPage /></ProtectedRoute>} />
      <Route path="/projects" element={<ProtectedRoute><MyProjectsPage /></ProtectedRoute>} />
    </Routes>
  )
}
