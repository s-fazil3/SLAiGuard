import { useContext } from "react";
import { Navigate } from "react-router-dom";
import { AuthContext } from "./AuthContext";

export default function ProtectedRoute({ children, adminOnly = false }) {
  const { token, profile, loading } = useContext(AuthContext);

  if (loading) {
    return <p>Loading...</p>;
  }

  if (!token) {
    return <Navigate to="/login" />;
  }

  if (adminOnly && (!profile || profile.role !== "ROLE_ADMIN")) {
    return <Navigate to="/dashboard" />;
  }

  return children;
}
