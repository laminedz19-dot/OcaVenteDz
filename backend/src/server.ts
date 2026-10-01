import dotenv from 'dotenv';
dotenv.config();

import app from './app';

const PORT = process.env.PORT || 4000;

app.listen(PORT, () => {
  console.log(`===============================================`);
  console.log(`🚀 ocaventeDz API running on http://localhost:${PORT}`);
  console.log(`🛡️  Zero-Firebase Architecture: Node.js + Prisma + PostgreSQL`);
  console.log(`===============================================`);
});
