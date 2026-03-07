import {
  ForbiddenException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import * as argon2 from 'argon2';
import { UsersService } from '../users/users.service.js';
import { RegisterDto } from './dto/register.dto.js';
import { LoginDto } from './dto/login.dto.js';
import { JwtStrategy } from '../common/strategies/jwt.strategy.js';
import { JwtRefreshStrategy } from '../common/strategies/jwt-refresh.strategy.js';

export interface Tokens {
  accessToken: string;
  refreshToken: string;
}

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly jwtStrategy: JwtStrategy,
    private readonly jwtRefreshStrategy: JwtRefreshStrategy,
  ) {}

  private async generateTokens(
    userId: string,
    email: string,
    role: string,
  ): Promise<Tokens> {
    const payload = { sub: userId, email, role };

    const [accessToken, refreshToken] = await Promise.all([
      this.jwtStrategy.signAsync(payload),
      this.jwtRefreshStrategy.signAsync(payload),
    ]);

    return { accessToken, refreshToken };
  }

  async register(dto: RegisterDto): Promise<Tokens> {
    const passwordHash = await argon2.hash(dto.password);
    const user = await this.usersService.create(
      dto.email,
      dto.username,
      passwordHash,
    );
    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }
    const tokens = await this.generateTokens(user.id, user.email, user.role);
    await this.updateRefreshToken(user.id, tokens.refreshToken);
    return tokens;
  }

  async login(dto: LoginDto): Promise<Tokens> {
    const user = await this.usersService.findByEmail(dto.email);
    if (!user || !user.isActive) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const passwordValid = await argon2.verify(user.password_hash, dto.password);
    if (!passwordValid) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const tokens = await this.generateTokens(user.id, user.email, user.role);
    return tokens;
  }

  async logout(userId: string): Promise<void> {
    await this.usersService.updateRefreshTokenHash(userId, null);
  }

  async refresh(userId: string, refreshToken: string): Promise<Tokens> {
    const user = await this.usersService.findById(userId);
    if (!user || !user.refreshToken) {
      throw new ForbiddenException('Access denied');
    }

    const tokenValid = await this.jwtRefreshStrategy.verifyAsync(refreshToken);
    if (tokenValid.sub !== userId) {
      throw new ForbiddenException('Access denied');
    }

    const tokens = await this.generateTokens(user.id, user.email, user.role);
    return tokens;
  }

  private async updateRefreshToken(
    userId: string,
    refreshToken: string,
  ): Promise<void> {
    const hash = await argon2.hash(refreshToken);
    await this.usersService.updateRefreshTokenHash(userId, hash);
  }
}
