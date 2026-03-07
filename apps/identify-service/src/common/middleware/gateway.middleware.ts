import {
  Injectable,
  NestMiddleware,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { NextFunction, Request, Response } from 'express';

@Injectable()
export class GatewayMiddleware implements NestMiddleware {
  constructor(private readonly configService: ConfigService) {}

  use(req: Request, _res: Response, next: NextFunction): void {
    const expectedSecret = this.configService.get<string>(
      'INTERNAL_GATEWAY_SECRET',
    );
    if (!expectedSecret) {
      throw new UnauthorizedException('Gateway secret is not configured');
    }

    const gatewaySecret = req.headers['x-internal-api-key'];
    const providedSecret = Array.isArray(gatewaySecret)
      ? gatewaySecret[0]
      : gatewaySecret;

    if (!providedSecret || providedSecret !== expectedSecret) {
      throw new UnauthorizedException('Only API Gateway can call this service');
    }

    next();
  }
}
