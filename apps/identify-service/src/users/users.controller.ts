import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Patch,
  UnauthorizedException,
  Request,
} from '@nestjs/common';
import type { Request as ExpressRequest } from 'express';
import { UpdateUserDto } from './dto/update-user.dto.js';
import { UsersService } from './users.service.js';
import { User } from '../common/entities/user.entity.js';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('me')
  async getProfile(@Request() req: ExpressRequest): Promise<User | null> {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    const found = await this.usersService.findById(user_id);
    if (!found) return null;
    return found;
  }

  @Patch('me')
  async updateProfile(
    @Request() req: ExpressRequest,
    @Body() dto: UpdateUserDto,
  ): Promise<User | null> {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    return this.usersService.updateUserInfo(user_id, dto);
  }

  @Patch('me/password')
  async changePassword(
    @Request() req: ExpressRequest,
    @Body() dto: { newPassword: string },
  ): Promise<void> {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    await this.usersService.changePassword(user_id, dto.newPassword);
  }

  @Delete('me')
  @HttpCode(HttpStatus.NO_CONTENT)
  async deleteAccount(@Request() req: ExpressRequest) {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    await this.usersService.softDelete(user_id);
  }
}
