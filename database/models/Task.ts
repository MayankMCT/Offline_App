import { Model } from '@nozbe/watermelondb';
import { date, field, readonly } from '@nozbe/watermelondb/decorators';


export class Task extends Model {
    static table = 'tasks';

    @field('name') name!: string;
    @field('is_completed') isCompleted!: boolean;
    @field('sync_status') syncStatus!: string; // 'pending' or 'synced'

    @readonly @date('created_at') createdAt!: Date;
    @readonly @date('updated_at') updatedAt!: Date;
}
