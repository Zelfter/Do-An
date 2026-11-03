# Decisions / assumptions requiring validation
- SRS §10.1 marks final React choice ⚠️ (React/Vue previously allowed). Default proposed React/Vite, require sign-off.
- Verify hosting/provider free-tier availability, quota, and privacy policies when deploying (§1.4, §10).
- NFR-01/02/05/13/25 are estimates/objectives requiring performance and evaluation measurements.
- Clarify token refresh persistence/revocation and session authorization error response if absent from SRS before implementing details.
- Confirm precise scoring config serialization/rounding and edge cases before locking API/DB contract.
- Document changes here with date, rationale, affected IDs and approval; don't silently alter SRS.
