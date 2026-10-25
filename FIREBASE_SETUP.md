# Firebase Auth + Cloud Firestore setup

The app uses Firebase Authentication for email/password sign-in and Cloud Firestore for user profiles. No Cloud Functions or Realtime Database are required.

1. In Firebase Authentication, enable **Email/Password**.
2. Cloud Firestore is already created for project `medicare-a4b4e`. Deploy the included Rules from the project root:

   ```bash
   firebase deploy --only firestore:rules
   ```

3. A public sign-up creates `users/{uid}` with this structure:

   ```text
   fullName: string
   email: string
   phone: string
   type: "USER"
   accountStatus: "ACTIVE"
   createdAt: timestamp
   updatedAt: timestamp
   ```

4. To provision a doctor or administrator for development, first create their email/password account in Firebase Authentication. Then, in Cloud Firestore create `users/{uid}` with the same fields and set `type` to `DOCTOR` or `ADMIN`.

Firestore Rules allow public registration to create `USER` only, and prevent a signed-in user from changing their own `type` or `accountStatus`.
