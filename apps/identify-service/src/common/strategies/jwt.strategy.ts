import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { StringValue } from 'ms';
import { UsersService } from 'src/users/users.service';

export interface JwtPayload {
  sub: string;
  email: string;
  role: string;
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy, 'jwt') {
  private readonly secret: string;
  private readonly expiresIn: StringValue;

  constructor(
    configService: ConfigService,
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
  ) {
    const secretKey = configService.getOrThrow<string>('JWT_ACCESS_SECRET');
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: secretKey,
    });
    this.secret = secretKey;
    this.expiresIn = configService.getOrThrow<StringValue>(
      'JWT_ACCESS_EXPIRATION',
      '15m',
    );
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

  public async validate(payload: JwtPayload): Promise<any> {
    const user = await this.usersService.findById(payload.sub);
    if (!user || !user.isActive) {
      throw new UnauthorizedException('Unauthorized');
    }
    return { id: user.id, email: user.email, role: user.role };
  }
}
