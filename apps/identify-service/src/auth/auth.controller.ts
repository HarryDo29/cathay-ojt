import {
  Body,
  Controller,
  HttpCode,
  HttpStatus,
  Post,
  Request,
  UnauthorizedException,
} from '@nestjs/common';
import type { Request as ExpressRequest } from 'express';
import { AuthService } from './auth.service.js';
import { LoginDto } from './dto/login.dto.js';
import { RegisterDto } from './dto/register.dto.js';
import { Tokens } from './auth.service.js';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register')
  async register(@Body() dto: RegisterDto): Promise<Tokens> {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  async login(@Body() dto: LoginDto): Promise<Tokens> {
    return this.authService.login(dto);
  }

  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  async refresh(@Request() req: ExpressRequest): Promise<Tokens> {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    const refresh_token = req.headers['authorization']?.split(' ')[1] as string;
    if (!refresh_token) {
      throw new UnauthorizedException('Unauthorized');
    }
    return this.authService.refresh(user_id, refresh_token);
  }

  @Post('logout')
  @HttpCode(HttpStatus.OK)
  async logout(@Request() req: ExpressRequest) {
    const user_id = req.headers['x-user-id'] as string;
    if (!user_id) {
      throw new UnauthorizedException('Unauthorized');
    }
    return this.authService.logout(user_id);
  }
}
