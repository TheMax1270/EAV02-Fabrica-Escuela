import { Navigate, Route, Routes } from 'react-router-dom'
import HomePage from '../pages/HomePage'
import RegisterPage from '../pages/RegisterPage'
import LoginPage from '../pages/LoginPage'
import AccountPage from '../pages/AccountPage'
import { useAuth } from '../context/useAuth'

function ProtectedAccount() {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <AccountPage /> : <Navigate to="/login" replace />
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/account" element={<ProtectedAccount />} />
    </Routes>
  )
}
