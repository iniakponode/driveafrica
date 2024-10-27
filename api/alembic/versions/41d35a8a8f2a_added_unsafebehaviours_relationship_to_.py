"""added unsafebehaviours relationship to location

Revision ID: 41d35a8a8f2a
Revises: a3f304f3cf61
Create Date: 2024-10-14 17:14:47.293603

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '41d35a8a8f2a'
down_revision: Union[str, None] = 'a3f304f3cf61'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
