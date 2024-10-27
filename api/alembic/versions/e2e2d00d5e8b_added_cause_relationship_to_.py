"""added cause relationship to unsafebehaviour

Revision ID: e2e2d00d5e8b
Revises: 1848a739a819
Create Date: 2024-10-14 17:27:12.992199

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e2e2d00d5e8b'
down_revision: Union[str, None] = '1848a739a819'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
