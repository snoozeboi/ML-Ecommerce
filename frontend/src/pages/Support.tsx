import { Mail, Phone, MessageCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const Support = () => {
  return (
    <div className="container py-16">
      <div className="max-w-lg mx-auto">
        <Card className="shadow-lg">
          <CardHeader className="text-center pb-2">
            <CardTitle className="text-3xl font-display">Contact Us!</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6 pt-6">
            {/* Email */}
            <div className="flex items-center gap-4 p-4 rounded-lg bg-secondary/50 hover:bg-secondary transition-colors">
              <div className="w-12 h-12 rounded-full bg-accent/10 flex items-center justify-center">
                <Mail className="w-6 h-6 text-accent" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Email</p>
                <a
                  href="mailto:support@ecommerce.com"
                  className="font-medium text-foreground hover:text-accent transition-colors"
                >
                  support@ecommerce.com
                </a>
              </div>
            </div>

            {/* WhatsApp */}
            <div className="flex items-center gap-4 p-4 rounded-lg bg-secondary/50 hover:bg-secondary transition-colors">
              <div className="w-12 h-12 rounded-full bg-success/10 flex items-center justify-center">
                <MessageCircle className="w-6 h-6 text-success" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">WhatsApp</p>
                <a
                  href="https://wa.me/1234567890"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-medium text-foreground hover:text-accent transition-colors"
                >
                  +1 (234) 567-890
                </a>
              </div>
            </div>

            {/* Phone */}
            <div className="flex items-center gap-4 p-4 rounded-lg bg-secondary/50 hover:bg-secondary transition-colors">
              <div className="w-12 h-12 rounded-full bg-info/10 flex items-center justify-center">
                <Phone className="w-6 h-6 text-info" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Customer Service</p>
                <a
                  href="tel:+18001234567"
                  className="font-medium text-foreground hover:text-accent transition-colors"
                >
                  +1 (800) 123-4567
                </a>
              </div>
            </div>

            {/* Support Hours */}
            <div className="text-center pt-4 border-t border-border">
              <p className="text-sm text-muted-foreground">
                Available Monday - Friday, 9 AM - 6 PM EST
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Support;
