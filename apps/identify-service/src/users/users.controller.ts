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

  @Get('test')
  async gatewayTest(@Request() req: ExpressRequest) {
    const ms = 500;
    if (ms > 0) {
      await new Promise<void>((resolve) => setTimeout(resolve, ms));
    }

    return {
      ok: true,
      service: 'identify-service',
      method: req.method,
      path: '/users/test',
      delayedMs: ms,
      timestamp: new Date().toISOString(),
      fromGateway: {
        'x-user-id': req.headers['x-user-id'] ?? null,
        'x-user-role': req.headers['x-user-role'] ?? null,
        'x-internal-api-key': req.headers['x-internal-api-key']
          ? '(set)'
          : null,
        'public-endpoint': req.headers['public-endpoint'] ?? null,
      },
    };
  }

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
