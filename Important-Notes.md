

1- We must start sequence ID for each entity (Patient/Doctor/Supervisor) with prefix to prevent overlapping
   that in some case we check based on function params (including id that can belong to doctor or supervisor)
   for example "getCustomPatientInformation" and the ids may intersects with each other.

2- 