import { appSchema, tableSchema } from '@nozbe/watermelondb';

export const schema = appSchema({
    version: 1,
    tables: [
        tableSchema({
            name: 'tasks',
            columns: [
                { name: 'name', type: 'string' },
                { name: 'is_completed', type: 'boolean' },
                { name: 'sync_status', type: 'string' }, // 'pending' or 'synced'
                { name: 'created_at', type: 'number' },
                { name: 'updated_at', type: 'number' },
            ],
        }),
    ],
});
