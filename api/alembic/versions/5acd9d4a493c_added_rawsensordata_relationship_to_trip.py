"""added rawsensordata relationship to trip

Revision ID: 5acd9d4a493c
Revises: d4a283449ed6
Create Date: 2024-10-14 17:23:43.458645

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '5acd9d4a493c'
down_revision: Union[str, None] = 'd4a283449ed6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
