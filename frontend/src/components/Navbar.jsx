import { Link, useLocation } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from '../auth/AuthContext';
import '../styles/navbar.css';
export default function Navbar() {
    const { profile, token, logout } = useContext(AuthContext);
    const location = useLocation();

    // Always show guest navbar on landing page
    const isLandingPage = location.pathname === '/';

    return (
        <nav className="navbar">
            <h2 className='logo'>SLAiGuard</h2>
            <div className='nav-links'>
                {(!token || isLandingPage) && (
                    <>
                        <Link to='/user/login'>User Login</Link>
                        <Link to='/admin/login'>Admin Login</Link>
                    </>
                )
                }
                {token && !isLandingPage && (
                    <>
                        <Link to={profile && profile.role === 'ROLE_ADMIN' ? '/admin/sla' : '/dashboard'}>Dashboard</Link>
                        <Link to='/alerts'>Alerts</Link>
                        <Link to='/profile'>Profile</Link>

                        <span className='username'>{profile?.username}</span>
                        <button onClick={logout}>Logout</button>
                    </>
                )}
            </div>
        </nav>
    );
}