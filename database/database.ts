import { Database } from '@nozbe/watermelondb';
import SQLiteAdapter from '@nozbe/watermelondb/adapters/sqlite';
import { Task } from './models/Task';
import { schema } from './schema';

// Create SQLite adapter
const adapter = new SQLiteAdapter({
    schema,
    // For debugging, you can enable:
    // jsi: false, // Use JSI for better performance (default in newer versions)
});

// Create database instance
export const database = new Database({
    adapter,
    modelClasses: [Task],
});
