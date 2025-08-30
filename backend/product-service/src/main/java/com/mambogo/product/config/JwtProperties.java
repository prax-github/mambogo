package com.mambogo.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private Claims claims = new Claims();
    private Roles roles = new Roles();
    private Authorities authorities = new Authorities();

    public Claims getClaims() {
        return claims;
    }

    public void setClaims(Claims claims) {
        this.claims = claims;
    }

    public Roles getRoles() {
        return roles;
    }

    public void setRoles(Roles roles) {
        this.roles = roles;
    }

    public Authorities getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Authorities authorities) {
        this.authorities = authorities;
    }

    public static class Claims {
        private String realmAccess = "realm_access";
        private String roles = "roles";
        private String preferredUsername = "preferred_username";
        private String email = "email";
        private String fullName = "name";

        public String getRealmAccess() {
            return realmAccess;
        }

        public void setRealmAccess(String realmAccess) {
            this.realmAccess = realmAccess;
        }

        public String getRoles() {
            return roles;
        }

        public void setRoles(String roles) {
            this.roles = roles;
        }

        public String getPreferredUsername() {
            return preferredUsername;
        }

        public void setPreferredUsername(String preferredUsername) {
            this.preferredUsername = preferredUsername;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    public static class Roles {
        private String admin = "ADMIN";
        private String user = "USER";

        public String getAdmin() {
            return admin;
        }

        public void setAdmin(String admin) {
            this.admin = admin;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }

    public static class Authorities {
        private String claimName = "realm_access.roles";
        private String prefix = "ROLE_";

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}
