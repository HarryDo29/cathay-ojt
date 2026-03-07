import {
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as argon2 from 'argon2';
import { Repository } from 'typeorm';
import { User } from '../common/entities/user.entity.js';
import { UpdateUserDto } from './dto/update-user.dto.js';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,
  ) {}

  async findById(id: string): Promise<User | null> {
    return this.usersRepository.findOne({ where: { id, isActive: true } });
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.usersRepository.findOne({ where: { email } });
  }

  async create(
    email: string,
    username: string,
    passwordHash: string,
  ): Promise<User | null> {
    const existing = await this.findByEmail(email);
    if (existing) {
      return null;
    }

    const user = this.usersRepository.create({
      email,
      username,
      password_hash: passwordHash,
    });
    return this.usersRepository.save(user);
  }

  async updateUserInfo(id: string, dto: UpdateUserDto): Promise<User> {
    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundException('User not found');
    }

    if (dto.email !== undefined) {
      const existing = await this.findByEmail(dto.email);
      if (existing && existing.id !== id) {
        throw new ConflictException('Email already in use');
      }
      user.email = dto.email;
    }

    if (dto.username !== undefined) {
      user.username = dto.username;
    }

    return this.usersRepository.save(user);
  }

  async changePassword(userId: string, newPassword: string): Promise<void> {
    const user = await this.findById(userId);
    if (!user) {
      throw new NotFoundException('User not found');
    }
    const hash = await this.hashPassword(newPassword);
    await this.usersRepository.update(userId, { password_hash: hash });
  }

  private async hashPassword(plainPassword: string): Promise<string> {
    return argon2.hash(plainPassword);
  }

  async softDelete(id: string): Promise<void> {
    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundException('User not found');
    }
    user.isActive = false;
    user.refreshToken = null;
    await this.usersRepository.save(user);
  }

  async updateRefreshTokenHash(id: string, hash: string | null): Promise<void> {
    await this.usersRepository.update(id, { refreshToken: hash });
  }
}
