import { Injectable, Logger, OnApplicationBootstrap } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as argon2 from 'argon2';
import { Repository } from 'typeorm';
import { User, UserRole } from '../entities/user.entity.js';

@Injectable()
export class SeedService implements OnApplicationBootstrap {
  private readonly logger = new Logger(SeedService.name);

  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
  ) {}

  async onApplicationBootstrap(): Promise<void> {
    const userCount = await this.usersRepository.count();
    if (userCount > 0) {
      this.logger.log(`${userCount} users already exist. Skipping seed data.`);
      return;
    }

    const seedUsers = [
      {
        role: UserRole.ADMIN,
        email: 'admin@cathay.com',
        username: 'admin',
        password: 'admin123',
      },
      {
        role: UserRole.USER,
        email: 'user@cathay.com',
        username: 'user',
        password: 'user123',
      },
    ];

    for (const item of seedUsers) {
      const passwordHash = await argon2.hash(item.password);
      const user = this.usersRepository.create({
        ...item,
        password_hash: passwordHash,
        isActive: true,
      });
      await this.usersRepository.save(user);
      this.logger.log(`Seeded ${item.role} user: ${item.email}`);
    }
  }
}
