import { createContext,useEffect,useState } from "react";
import api from "../api/api";

export const AuthContext = createContext();

export function AuthProvider({ children }) {
    const[token,setToken]=useState(localStorage.getItem("token"));
    const[loading,setLoading]=useState(true);
    const[profile,setProfile]=useState(null);

    useEffect(()=>{
        if(token){
            fetchProfile();
        }
        else{
            setLoading(false);
        }  },[token]); 

        const fetchProfile=async()=>{
            try{
                const res=await api.get("/api/profile");
                setProfile(res.data);
                return res.data;
            }
            catch{
                logout();
                return null;
            }
            finally{
                setLoading(false);
            }
        };

        const login = async(jwt)=>{
            localStorage.setItem("token",jwt);  
            setToken(jwt);
            const profileData = await fetchProfile();
            return profileData;
        }

        const logout=()=>{
            localStorage.removeItem("token");
            setToken(null);
            setProfile(null);
        }

        return(
            <AuthContext.Provider value={{token,profile,login,logout,loading}}>
                {children}
            </AuthContext.Provider>
        );

}