import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { PassportStrategy } from '@nestjs/passport';
import { Request } from 'express';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { StringValue } from 'ms';
import { JwtPayload } from './jwt.strategy';

@Injectable()
export class JwtRefreshStrategy extends PassportStrategy(
  Strategy,
  'jwt-refresh',
) {
  private readonly secret: string;
  private readonly expiresIn: StringValue;

  constructor(
    configService: ConfigService,
    private readonly jwtService: JwtService,
  ) {
    const secret = configService.getOrThrow<string>('JWT_REFRESH_SECRET');
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      secretOrKey: secret,
      passReqToCallback: true,
    });
    this.secret = secret;
    this.expiresIn = configService.get<string>(
      'JWT_REFRESH_EXPIRATION',
      '7d',
    ) as StringValue;
  }

  public async signAsync(payload: JwtPayload): Promise<string> {
    return this.jwtService.signAsync(payload, {
      secret: this.secret,
      expiresIn: this.expiresIn,
    });
  }

  public async verifyAsync(token: string): Promise<JwtPayload> {
    return this.jwtService.verifyAsync<JwtPayload>(token, {
      secret: this.secret,
    });
  }

  public validate(req: Request, payload: JwtPayload) {
    const refreshTokenHeader = req.get('Authorization');
    const refreshToken = refreshTokenHeader?.split(' ')[1].trim();
    if (!refreshToken) {
      throw new UnauthorizedException('Unauthorized');
    }
    return { payload, refreshToken };
  }
}
